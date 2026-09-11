package com.svarax.history

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * CallHistoryRepository: Local persistence for analyzed call records.
 *
 * Strictly prevents unprompted population of fabricated historical calls.
 * Default history returns an empty list ("No analyzed calls yet") until real
 * calls or explicit user-requested demo seeds are logged.
 */
class CallHistoryRepository(context: Context) {

    companion object {
        private const val TAG = "SvaraX_HistoryRepo"
        private const val PREFS_NAME = "svara_call_history_prefs_v2"
        private const val KEY_RECORDS = "key_history_records"
        private const val MAX_RECORDS = 50
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveRecord(record: CallRecord) {
        try {
            val existing = getAllRecords().toMutableList()
            // Prepend newest record
            existing.add(0, record)

            val trimmed = if (existing.size > MAX_RECORDS) existing.take(MAX_RECORDS) else existing
            val jsonArray = JSONArray()

            for (item in trimmed) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("callerNumber", item.callerNumber)
                    put("durationSeconds", item.durationSeconds)
                    put("finalRiskScore", item.finalRiskScore)
                    put("riskLevel", item.riskLevel)
                    put("recommendation", item.recommendation)
                    put("fullTranscriptSnippet", item.fullTranscriptSnippet)
                    put("isDemoSimulation", item.isDemoSimulation)

                    val indArray = JSONArray()
                    for (ind in item.detectedIndicators) {
                        indArray.put(ind)
                    }
                    put("detectedIndicators", indArray)
                }
                jsonArray.put(obj)
            }

            prefs.edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
            Log.i(TAG, "Call record saved: id=${record.id}, caller=${record.callerNumber}, score=${record.finalRiskScore}%, isDemo=${record.isDemoSimulation}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save call record", e)
        }
    }

    @Synchronized
    fun getAllRecords(): List<CallRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        val list = mutableListOf<CallRecord>()

        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val indList = mutableListOf<String>()
                val indArray = obj.optJSONArray("detectedIndicators")
                if (indArray != null) {
                    for (j in 0 until indArray.length()) {
                        indList.add(indArray.getString(j))
                    }
                }

                list.add(
                    CallRecord(
                        id = obj.getString("id"),
                        timestamp = obj.getLong("timestamp"),
                        callerNumber = obj.getString("callerNumber"),
                        durationSeconds = obj.getInt("durationSeconds"),
                        finalRiskScore = obj.getInt("finalRiskScore"),
                        riskLevel = obj.getString("riskLevel"),
                        detectedIndicators = indList,
                        recommendation = obj.getString("recommendation"),
                        fullTranscriptSnippet = obj.optString("fullTranscriptSnippet", ""),
                        isDemoSimulation = obj.optBoolean("isDemoSimulation", false)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse call records JSON", e)
        }

        return list
    }

    /**
     * Seeds sample records strictly when explicitly requested via Settings -> Demo & Testing.
     * Every record is clearly marked [DEMO] and isDemoSimulation = true.
     */
    @Synchronized
    fun seedDemoAuditRecords() {
        val now = System.currentTimeMillis()
        val demoRecords = listOf(
            CallRecord(
                id = "demo-rec-1",
                timestamp = now - 1000L * 60 * 25,
                callerNumber = "[DEMO] +91 98230 11942",
                durationSeconds = 64,
                finalRiskScore = 95,
                riskLevel = "CRITICAL",
                detectedIndicators = listOf(
                    "Bank impersonation",
                    "Account threat detected",
                    "Urgency detected",
                    "OTP request detected"
                ),
                recommendation = "DO NOT SHARE OTP OR PIN! HANG UP IMMEDIATELY. OFFICIAL BANKS NEVER ASK FOR PASSWORDS.",
                fullTranscriptSnippet = "[Simulated Demo] I am calling from your bank branch security desk. Your account will be blocked within ten minutes. Please tell me the OTP you just received.",
                isDemoSimulation = true
            ),
            CallRecord(
                id = "demo-rec-2",
                timestamp = now - 1000L * 60 * 60 * 3,
                callerNumber = "[DEMO] +91 98450 44321",
                durationSeconds = 120,
                finalRiskScore = 5,
                riskLevel = "LOW",
                detectedIndicators = emptyList(),
                recommendation = "No immediate fraud pattern detected. Maintain standard vigilance.",
                fullTranscriptSnippet = "[Simulated Demo] Hi Dad, just checking if you reached home safely. Let me know when you are free for dinner.",
                isDemoSimulation = true
            ),
            CallRecord(
                id = "demo-rec-3",
                timestamp = now - 1000L * 60 * 60 * 24,
                callerNumber = "[DEMO] +91 80001 23456",
                durationSeconds = 85,
                finalRiskScore = 78,
                riskLevel = "HIGH",
                detectedIndicators = listOf(
                    "Remote access app request",
                    "Urgent KYC update demand"
                ),
                recommendation = "DO NOT INSTALL ANY SCREEN-SHARING APP! DISCONNECT THE CALL NOW.",
                fullTranscriptSnippet = "[Simulated Demo] Your KYC has expired. Download AnyDesk immediately so our representative can assist your verification.",
                isDemoSimulation = true
            )
        )

        for (rec in demoRecords) {
            saveRecord(rec)
        }
        Log.i(TAG, "Sample [DEMO] audit records seeded for testing")
    }

    @Synchronized
    fun clearHistory() {
        prefs.edit().remove(KEY_RECORDS).apply()
        Log.i(TAG, "Call history cleared cleanly")
    }
}
