package com.svarax.history

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Priority 12: Lightweight local history repository.
 * Persists recent call assessments locally for audit and hackathon demonstration.
 */
class CallHistoryRepository(context: Context) {

    companion object {
        private const val TAG = "SvaraX_HistoryRepo"
        private const val PREFS_NAME = "svara_call_history_prefs"
        private const val KEY_RECORDS = "key_history_records"
        private const val MAX_RECORDS = 50
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveRecord(record: CallRecord) {
        try {
            val existing = getAllRecords().toMutableList()
            // Add new record at top
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
            Log.i(TAG, "Call record saved: id=${record.id}, score=${record.finalRiskScore}%")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save call record", e)
        }
    }

    @Synchronized
    fun getAllRecords(): List<CallRecord> {
        val raw = prefs.getString(KEY_RECORDS, null)
        if (raw == null) {
            val defaults = createDefaultAuditRecords()
            defaults.forEach { saveRecord(it) }
            return defaults
        }
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

    private fun createDefaultAuditRecords(): List<CallRecord> {
        val now = System.currentTimeMillis()
        return listOf(
            CallRecord(
                id = "audit-rec-1",
                timestamp = now - 1000L * 60 * 25, // 25 mins ago
                callerNumber = "+91 98230 11942",
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
                fullTranscriptSnippet = "I am calling from your bank branch security desk. Your account will be blocked within ten minutes. Please tell me the OTP you just received.",
                isDemoSimulation = true
            ),
            CallRecord(
                id = "audit-rec-2",
                timestamp = now - 1000L * 60 * 60 * 3, // 3 hours ago
                callerNumber = "+91 98450 44321",
                durationSeconds = 120,
                finalRiskScore = 12,
                riskLevel = "LOW",
                detectedIndicators = emptyList(),
                recommendation = "No immediate fraud pattern detected. Maintain standard vigilance.",
                fullTranscriptSnippet = "Hi Dad, just checking if you reached home safely. Let me know when you are free for dinner.",
                isDemoSimulation = false
            ),
            CallRecord(
                id = "audit-rec-3",
                timestamp = now - 1000L * 60 * 60 * 24, // yesterday
                callerNumber = "+91 80001 23456",
                durationSeconds = 85,
                finalRiskScore = 78,
                riskLevel = "HIGH",
                detectedIndicators = listOf(
                    "Remote access app request",
                    "Urgent KYC update demand"
                ),
                recommendation = "DO NOT INSTALL ANY SCREEN-SHARING APP! DISCONNECT THE CALL NOW.",
                fullTranscriptSnippet = "Your KYC has expired. Download AnyDesk immediately so our representative can assist your verification.",
                isDemoSimulation = true
            )
        )
    }

    @Synchronized
    fun clearHistory() {
        prefs.edit().remove(KEY_RECORDS).apply()
    }
}
